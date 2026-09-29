# Start backend jar and frontend dev server as detached processes (ASCII only)
$root = "d:\project\CjluJwc\CjluJwc"
$env:DB_PASSWORD = "root"

# Backend (JDK 17, port 8080)
$java = "C:\Program Files\Java\jdk-17\bin\java.exe"
Start-Process -FilePath $java `
  -ArgumentList '-jar', 'build-temp\yu-admin\yu-admin.jar', '--server.port=8080' `
  -WorkingDirectory $root -WindowStyle Hidden `
  -RedirectStandardOutput "$root\output\backend-out.log" `
  -RedirectStandardError "$root\output\backend-err.log"

# Frontend yu-ui-vue3 dev server (port 82; U6: Vue2 yu-ui archived, entry switched to Vue3)
Start-Process -FilePath "cmd.exe" `
  -ArgumentList '/c', 'npm run dev' `
  -WorkingDirectory "$root\yu-ui-vue3" -WindowStyle Hidden `
  -RedirectStandardOutput "$root\output\frontend-out.log" `
  -RedirectStandardError "$root\output\frontend-err.log"

Write-Output "launch issued"
