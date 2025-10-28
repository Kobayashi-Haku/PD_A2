### **GitHub上のリポジトリをPCに接続する方法（これは始めの1回だけで良い）**
PCにプロジェクト用フォルダを作成しvscodeでそのフォルダを開く。ターミナルを開き、```git clone https://github.com/Kobayashi-Haku/PD_A2```を実行

### **ブランチをPCに保存する方法**
PCのプロジェクト用フォルダからターミナルを開き以下のコマンドを実行  
```git fetch```  
```git checkout ブランチ名```

### **PCで編集したコードをPCのGitに保存する方法**
```git add .```  
```git commit -m "ここに変更点や追加した機能などのメッセージを書く"```

### **既存のブランチに保存する方法**
まだの場合変更内容をコミットする（上記）  
```git push```  

### **新しいブランチとして保存する方法**  
まだの場合変更内容をコミットする（上記）  
```git checkout -b 新規ブランチ名```  
```git push -u origin 新規ブランチ名```

### **大学内でpushする場合**  
以下を実行してから```git push```  
```git config --global http.proxy http://wwwproxy.kanazawa-it.ac.jp:8080```  
```git config --global https.proxy http://wwwproxy.kanazawa-it.ac.jp:8080```  

### **大学外でpushする場合**  
以下を実行してから```git push```  
```git config --global --unset http.proxy```  
```git config --global --unset https.proxy```  

### **アプリ起動方法**
プロジェクト用フォルダからターミナルを開き```mvn spring-boot:run```を実行し、ブラウザで`http://localhost:8080/`を開くとwebアプリが起動する。

### **メール通知機能の設定方法**
このアプリケーションは食品の消費期限通知をGmailで送信できます。

#### 1. Gmail設定
1. Googleアカウントで2段階認証を有効にする
2. Googleアカウントの「セキュリティ」→「アプリパスワード」で新しいアプリパスワードを生成
3. 16桁のアプリパスワードをメモしておく

#### 2. 環境変数設定
1. `.env.example`をコピーして`.env`ファイルを作成
2. `.env`ファイルを開き、以下の値を実際の情報に変更：
   ```
   MAIL_USERNAME=あなたのGmailアドレス
   MAIL_PASSWORD=16桁のアプリパスワード
   ```

#### 3. Windows環境での起動
1. `setup-env.bat`を実行して環境変数を設定
2. `mvn spring-boot:run`でアプリケーションを起動

#### 4. 手動での環境変数設定（PowerShell）
```powershell
$env:MAIL_USERNAME="あなたのGmailアドレス"
$env:MAIL_PASSWORD="16桁のアプリパスワード"
mvn spring-boot:run
```

#### メール通知機能
- 消費期限が翌日の食品を登録すると即座にメール通知
- 毎日朝9時に消費期限が近い食品の一覧をメール送信

**注意**: `.env`ファイルは個人の認証情報を含むため、GitHubにはpushされません。
