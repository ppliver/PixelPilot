# CI Debugging Patterns for Android Builds

## Problem: Hidden Java Compilation Errors

When Gradle reports:
```
Execution failed for task ':app:compileReleaseJavaWithJavac'.
> Compilation failed; see the compiler error output for details.
```

The actual Java compilation errors are hidden by Gradle's summary output.

## Solution: Get Verbose Build Output

Add `--info` to the gradle command for verbose output:
```yaml
- name: Build APK (verbose)
  run: |
    ./gradlew assembleRelease \
      --info \
      -Pandroid.injected.signing.store.file=${{ runner.temp }}/pixelpilot.jks \
      ...
```

Or capture errors separately:
```yaml
- name: Build APK
  run: |
    ./gradlew assembleRelease ... 2>&1 | tee build.log

- name: Show Compilation Errors
  if: failure()
  run: grep -A 20 "error:" build.log | head -50
```

## Problem: Keystore Invalid/Empty Build Failure

**Error signature**:
```
KeytoolException: Failed to read key pixelpilot from store "...": 
Tag number over 30 is not supported
```

**Cause**: The base64-encoded keystore secret is empty, corrupted, or in an incompatible format.

**Solution**: Validate keystore before signing:
```yaml
- name: Build APK
  run: |
    # Check if keystore exists and is valid JKS
    if [ -f "${{ runner.temp }}/pixelpilot.jks" ] && \
       file "${{ runner.temp }}/pixelpilot.jks" | grep -qE "Java KeyStore|keytool|data"; then
      ./gradlew assembleRelease \
        -Pandroid.injected.signing.store.file=... \
        -Pandroid.injected.signing.store.password=... \
        -Pandroid.injected.signing.key.alias=pixelpilot \
        -Pandroid.injected.signing.key.password=...
    else
      echo "Invalid keystore, building unsigned APK"
      ./gradlew assembleRelease
    fi
```

## Getting CI Logs Without gh CLI

### Method 1: GitHub REST API
```bash
# Get job list
curl -s -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$RUN_ID/jobs"

# Download logs as zip
curl -s -L -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$RUN_ID/logs" \
  -o /tmp/logs.zip

# Extract and search
unzip -p /tmp/logs.zip job-XXXXX.log | grep "error:"
```

### Method 2: Direct Log URL (if available)
Workflows may expose direct blob storage URLs for logs:
```
https://productionresultssaXX.blob.core.windows.net/actions-results/.../job-logs.txt
```

These URLs are temporary and require no auth.

### Method 3: Workflow Run Summary Page
When API is unavailable, ask user to paste:
- The error output from the workflow run page
- Or click on the failed job → scroll to the error section

## Common Java Compilation Issues

### Cannot find symbol: Class/Variable
**Check**:
1. Import statement correct?
2. Class exists in the right module/package?
3. Variable declared before use?

### Wrong import (android.net vs java.net)
```java
// Wrong
import android.net.DatagramSocket;

// Correct
import java.net.DatagramSocket;
```

## Verification Steps

1. Add `if: always()` debug step to check APK output:
```yaml
- name: List APK Output
  if: always()
  run: |
    find app/build/outputs -name "*.apk" -type f || echo "No APK found"
    ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "Directory not found"
```

2. Distinguish between:
   - Build failed before APK generation → fix compilation errors
   - Build succeeded but wrong path → verify output location
