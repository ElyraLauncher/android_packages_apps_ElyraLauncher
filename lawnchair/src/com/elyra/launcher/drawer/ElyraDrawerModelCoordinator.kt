/*
 * Copyright 2026, The Elyra Launcher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.elyra.launcher.drawer

import android.content.Context
import android.os.Trace
import com.android.launcher3.BuildConfig
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.util.Executors
import com.elyra.launcher.allapps.ElyraAppIconColorExtractor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Reconciles drawer-owned caches against immutable Launcher3 model snapshots.
 * Work stays on the existing model executor and only the newest generation may
 * publish a rebind request to the main thread.
 */
object ElyraDrawerModelCoordinator {
    private const val PACKAGE_EVENT_COALESCE_MS = 150L

    data class AppSignature(
        val componentKey: String,
        val packageName: String,
        val label: String,
        val iconIdentity: Int,
    )

    private val generation = AtomicLong()
    private val signatures = ConcurrentHashMap<String, AppSignature>()
    private val pendingLock = Any()
    private var pendingReconcile: PendingReconcile? = null
    private val drainPending = Runnable {
        val pending = synchronized(pendingLock) {
            pendingReconcile.also { pendingReconcile = null }
        } ?: return@Runnable
        reconcileSnapshot(pending)
    }

    private data class PendingReconcile(
        val request: Long,
        val context: Context,
        val apps: List<AppInfo>,
        val onReady: Runnable,
    )

    @JvmStatic
    fun reconcile(context: Context, apps: List<AppInfo>, onReady: Runnable) {
        val request = generation.incrementAndGet()
        synchronized(pendingLock) {
            pendingReconcile = PendingReconcile(
                request,
                context.applicationContext,
                apps.toList(),
                onReady,
            )
        }
        Executors.MODEL_EXECUTOR.handler.removeCallbacks(drainPending)
        Executors.MODEL_EXECUTOR.handler.postDelayed(drainPending, PACKAGE_EVENT_COALESCE_MS)
    }

    private fun reconcileSnapshot(pending: PendingReconcile) {
        if (BuildConfig.DEBUG) Trace.beginSection("ElyraPackageBatchReconcile")
        try {
            val next = pending.apps.associateBy(
                keySelector = { it.toComponentKey().toString() },
                valueTransform = { app ->
                    AppSignature(
                        componentKey = app.toComponentKey().toString(),
                        packageName = app.targetPackage.orEmpty(),
                        label = app.title?.toString().orEmpty(),
                        iconIdentity = System.identityHashCode(app.bitmap),
                    )
                },
            )
            val changed = changedKeys(signatures, next)

            if (changed.isNotEmpty()) {
                val removed = signatures.keys.filterTo(HashSet()) { it !in next }
                ElyraCategoryCardModel.invalidate(changed)
                ElyraAppIconColorExtractor.invalidate(changed)
                ElyraDrawerSuggestions.invalidate(changed)
                ElyraDrawerSuggestions.removeUsage(pending.context, removed)
                val affectedApps = pending.apps.filter { it.toComponentKey().toString() in changed }
                ElyraCategoryCardModel.warm(pending.context, affectedApps)
                ElyraDrawerSuggestions.warmInstallTimes(pending.context, affectedApps)
                signatures.clear()
                signatures.putAll(next)
            }

            if (generation.get() == pending.request) {
                Executors.MAIN_EXECUTOR.execute {
                    if (generation.get() == pending.request && changed.isNotEmpty()) {
                        if (BuildConfig.DEBUG) Trace.beginSection("ElyraPackageBatchApply")
                        try {
                            pending.onReady.run()
                        } finally {
                            if (BuildConfig.DEBUG) Trace.endSection()
                        }
                    }
                }
            }
        } finally {
            if (BuildConfig.DEBUG) Trace.endSection()
        }
    }

    @JvmStatic
    fun currentGeneration(): Long = generation.get()

    @JvmStatic
    fun cancelPending() {
        generation.incrementAndGet()
        Executors.MODEL_EXECUTOR.handler.removeCallbacks(drainPending)
        synchronized(pendingLock) { pendingReconcile = null }
    }

    /** Pure reconciliation policy used by package-model tests. */
    @JvmStatic
    fun changedKeys(
        previous: Map<String, AppSignature>,
        next: Map<String, AppSignature>,
    ): Set<String> = buildSet {
        previous.forEach { (key, value) ->
            if (next[key] != value) add(key)
        }
        next.forEach { (key, value) ->
            if (previous[key] != value) add(key)
        }
    }

    @JvmStatic
    fun resetForTests() {
        cancelPending()
        generation.set(0)
        signatures.clear()
    }
}
