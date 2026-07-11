# Security Policy

Elyra Launcher is an Android home-screen application derived from the AOSP
Launcher3 / Quickstep base with additional Elyra features. Because a launcher is
a highly privileged surface on a device — it resolves intents, hosts widgets,
can be granted platform permissions in a ROM-integrated build, and mediates
access to the app list — we take security reports seriously.

## Supported versions

Security fixes are provided for the latest released version and for the current
development branch (`main`). Older tagged releases are not maintained; please
reproduce on a current build before reporting.

| Version                     | Supported          |
| --------------------------- | ------------------ |
| Latest release              | :white_check_mark: |
| `main` (development)        | :white_check_mark: |
| Older tagged releases       | :x:                |

## Reporting a vulnerability

**Please do not open a public issue for a security vulnerability.** A public
issue discloses an exploitable weakness before a fix is available and puts users
at risk.

Instead, use **GitHub private vulnerability reporting**:

1. Go to the repository's **Security** tab.
2. Select **Report a vulnerability** (Security advisories → *Report a
   vulnerability*).
3. Provide the details below.

If private reporting is unavailable to you, open a minimal, non-exploit issue
that only says you have a security report and requests a private channel — do
not include reproduction details in the public issue.

### What to include

- Affected build: universal APK or ROM-integrated build, and the version /
  commit SHA.
- Device, Android version, and (if ROM-integrated) the ROM name and build.
- A clear description of the weakness and its impact.
- Minimal reproduction steps or a proof of concept.
- Any relevant logcat, intent, or component details.

### What to expect

- We aim to acknowledge reports promptly and to keep you updated as we
  investigate. We do not promise a fixed response-time SLA.
- We will confirm the issue, assess severity, prepare a fix, and coordinate
  disclosure. Credit is offered to reporters who wish to be named.
- Please allow a reasonable period for a fix before any public disclosure.

## Launcher-specific vulnerability categories

The following categories are especially relevant to a launcher and are in scope:

- **Exported component exposure** — activities, services, receivers, or
  providers reachable by other apps that should not be.
- **Intent spoofing / injection** — untrusted intents or extras causing
  privileged actions, redirection, or state changes.
- **Privilege misuse** — misuse of granted permissions or launcher position to
  access data or actions beyond intent.
- **ROM-only permission misuse** — abuse of signature/privileged permissions
  that are only available in a ROM-integrated build.
- **Hidden-app bypass** — revealing or launching apps the user hid through a
  path that skips the intended gate.
- **Biometric-gate bypass** — reaching gated content without satisfying the
  biometric/authentication requirement.
- **Unsafe backup or restore** — importing/exporting launcher data in a way that
  leaks data or allows injection of malicious state.
- **Package or usage-history leakage** — exposing installed-app lists, usage,
  or suggestion signals to other apps or logs.
- **Unsafe URI or file handling** — path traversal, arbitrary file read/write,
  or unvalidated `content://` / `file://` handling.
- **Signing or update-chain compromise** — weaknesses in the release signing or
  update path that could allow tampered builds.

## Out of scope

- Vulnerabilities in the upstream AOSP Launcher3 / Quickstep base that are not
  introduced or amplified by Elyra changes (report those upstream), though we
  still appreciate a heads-up.
- Issues requiring a rooted/compromised device or a malicious ROM the user
  installed themselves.
- Theoretical reports without a plausible impact on a real build.

## Good-faith research

We support good-faith security research. Do not access other users' data, do not
degrade service for others, and give us a reasonable opportunity to remediate
before disclosure.
