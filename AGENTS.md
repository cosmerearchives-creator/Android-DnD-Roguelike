# Development handoff
This is a minimal Android game starter. The user will direct game development from their phone.
Do not invent game mechanics or select an engine until requested.
Preserve com.cosmerearchives.dndroguelike as the application ID and the existing signing identity.
For releases increment versionCode and versionName in version.properties.
Stable updates must track main. Keep automatic launch checks, manual checks, and installation flow functional.
Never commit signing keys. Release signing uses the SIGNING_KEYSTORE_BASE64 GitHub Actions secret.
Run assembleDebug and lintDebug before release; signed APKs must pass apksigner verify.
