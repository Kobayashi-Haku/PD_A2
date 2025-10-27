### **GitHub上の既存リポジトリを、PCに持ってくる方法（これは始めの1回だけで良い）**
PCにプロジェクト用フォルダを作成し、vscodeでそのフォルダを開く。ターミナルを開き、```git clone https://github.com/Kobayashi-Haku/PD_A2```を実行

### **ブランチをPCに保存する方法**
PCのプロジェクト用フォルダからターミナルを開き、以下のコマンドを実行  
```git fetch```  
```git checkout ブランチ名```

### **アプリ起動方法**
プロジェクト用フォルダからターミナルを開き```mvn spring-boot:run```を実行し、ブラウザで`http://localhost:8080/`を開くとwebアプリが起動する。
