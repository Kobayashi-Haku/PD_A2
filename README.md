### **GitHub上のリポジトリをPCに接続する方法（これは始めの1回だけで良い）**
PCにプロジェクト用フォルダを作成しvscodeでそのフォルダを開く。ターミナルを開き、```git clone https://github.com/Kobayashi-Haku/PD_A2```を実行

### **ブランチをPCに保存する方法**
PCのプロジェクト用フォルダからターミナルを開き以下のコマンドを実行  
```git fetch```  
```git checkout ブランチ名```

### **PCで編集したコードをPCのGitに保存する方法**
```git add .```  
```git commit -m "ここに変更点や追加した機能などのメッセージを書く"```

### **ブランチを最新のものに更新する方法**  
他の人が編集しpushしたブランチをローカルPCに保存する場合は以下を実行  
```git pull```

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
```https://maven.apache.org/download.cgi```のBinary zip archive	apache-maven-3.9.11-bin.zipをダウンロード  
プロジェクト用フォルダからターミナルを開き```mvn spring-boot:run```を実行し、ブラウザで`http://localhost:8080/`を開くとwebアプリが起動する。

### **github copilot学生認証**  
```https://github.com/education?locale=ja```
