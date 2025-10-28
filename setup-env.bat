@echo off
echo メール通知設定用環境変数を設定します...

if not exist .env (
    echo .envファイルが見つかりません。
    echo .env.exampleファイルをコピーして.envファイルを作成し、実際の値を設定してください。
    pause
    exit /b 1
)

echo .envファイルから環境変数を読み込んでいます...

for /f "usebackq tokens=1,2 delims==" %%i in (".env") do (
    if not "%%i"=="" if not "%%i"=="#" (
        set "%%i=%%j"
        echo 設定: %%i
    )
)

echo.
echo 環境変数の設定が完了しました。
echo 以下のコマンドでアプリケーションを起動してください：
echo mvn spring-boot:run
echo.
pause