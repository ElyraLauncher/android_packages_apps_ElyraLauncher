/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.elyra.launcher.drawer

import java.util.concurrent.atomic.AtomicInteger

/** Lets only the newest asynchronous drawer request publish its result. */
class ElyraRequestGeneration {
    private val generation = AtomicInteger()

    fun next(): Int = generation.incrementAndGet()

    fun cancel() {
        generation.incrementAndGet()
    }

    fun isCurrent(request: Int): Boolean = generation.get() == request
}
