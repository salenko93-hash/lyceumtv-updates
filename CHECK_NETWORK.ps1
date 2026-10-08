$ErrorActionPreference = "Continue"

$urls = @(
  "https://cache-redirector.jetbrains.com/maven-central/org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.pom",
  "https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/8.8.2/gradle-8.8.2.pom",
  "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-8.10.2-bin.zip"
)

foreach ($url in $urls) {
    Write-Host ""
    Write-Host "Testing: $url"
    try {
        $request = [System.Net.HttpWebRequest]::Create($url)
        $request.Method = "GET"
        $request.Timeout = 30000
        $request.AllowAutoRedirect = $true
        $response = $request.GetResponse()
        Write-Host "HTTP $([int]$response.StatusCode) OK" -ForegroundColor Green
        $response.Close()
    } catch {
        $code = $null
        if ($_.Exception.Response) {
            try { $code = [int]$_.Exception.Response.StatusCode } catch {}
        }
        if ($code) {
            Write-Host "HTTP $code FAILED" -ForegroundColor Red
        } else {
            Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "WinHTTP proxy:"
netsh winhttp show proxy
