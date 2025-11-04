# 食品管理システム (Food Manager)

## プロジェクト概要

食品の消費期限を管理し、期限が近づいた際に通知機能を提供するWebアプリケーションシステムです。ユーザーは登録した食品の一覧表示、追加、編集、削除が可能で、メール通知機能により食品の無駄を防ぐことができます。

## システム仕様

### 技術スタック

#### バックエンド
- **Java**: 21
- **Spring Boot**: 3.2.0
- **Spring Security**: 認証・認可機能
- **Spring Data JPA**: データ永続化
- **Spring Mail**: メール送信機能
- **Maven**: ビルドツール

#### フロントエンド
- **Thymeleaf**: テンプレートエンジン
- **Bootstrap**: 5.3.2（WebJarsで管理）
- **HTML5/CSS3**: UI構築

#### データベース
- **H2 Database**: 開発・本番環境（ファイルベース）
- **MySQL**: オプション対応

#### その他
- **Lombok**: Javaコードの簡素化
- **Spring Boot DevTools**: 開発効率向上

### 機能仕様

#### 1. ユーザー管理機能
- **ユーザー登録**: メールアドレス、ユーザー名、パスワードによる新規登録
- **ログイン/ログアウト**: Spring Securityによる認証
- **セッション管理**: 30分のセッションタイムアウト

#### 2. 食品管理機能
- **食品登録**: 食品名、消費期限の入力による新規登録
- **食品一覧表示**: ユーザー固有の食品リスト表示
- **食品編集**: 既存食品の名前・消費期限変更
- **食品削除**: 不要な食品の削除
- **統計表示**: 
  - 総食品数
  - 3日以内期限切れ予定の警告食品数
  - 期限切れ食品数

#### 3. 通知機能
- **即座通知**: 消費期限が明日の食品登録時の即座メール送信
- **定期通知**: 毎日午前9時の自動通知チェック
- **通知条件**: 食品登録から1日経過 & 未通知の食品
- **メール設定**: Gmail SMTP使用（環境変数での設定）

#### 4. セキュリティ機能
- **認証機能**: Spring Securityによるフォームベース認証
- **認可機能**: ユーザー固有データへのアクセス制御
- **パスワード暗号化**: BCryptPasswordEncoder使用
- **CSRF保護**: Spring Securityの標準機能

### データベース設計

#### Users テーブル
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- username (VARCHAR, UNIQUE, NOT NULL)
- email (VARCHAR, UNIQUE, NOT NULL)
- password (VARCHAR, NOT NULL)
- role (VARCHAR, DEFAULT 'ROLE_USER')
```

#### Foods テーブル
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- name (VARCHAR, NOT NULL)
- expiration_date (DATE, NOT NULL)
- registered_at (TIMESTAMP, NOT NULL)
- notification_sent (BOOLEAN, DEFAULT FALSE)
- user_id (BIGINT, FOREIGN KEY)
```

### API エンドポイント

#### 認証関連
- `GET /login` - ログインページ表示
- `POST /login` - ログイン処理（Spring Security）
- `GET /register` - 登録ページ表示
- `POST /register` - ユーザー登録処理
- `POST /logout` - ログアウト処理

#### 食品管理関連
- `GET /` - 食品一覧ページ（ダッシュボード）
- `GET /add` - 食品追加フォーム表示
- `POST /add` - 食品追加処理
- `GET /edit/{id}` - 食品編集フォーム表示
- `POST /update` - 食品更新処理
- `POST /delete` - 食品削除処理

### 設定仕様

#### アプリケーション設定 (application.properties)
```properties
# データベース設定
spring.datasource.url=jdbc:h2:file:./data/fooddb;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=

# サーバー設定
server.address=0.0.0.0
server.port=8080

# メール設定
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# 通知設定
app.notification.enabled=true

# セッション設定
server.servlet.session.timeout=30m
```

### スケジューリング仕様

#### 通知スケジューラー
- **実行時刻**: 毎日午前9時（Cron: `0 0 9 * * *`）
- **処理内容**: 
  1. 登録から1日経過した未通知食品を検索
  2. 該当食品のユーザーにメール通知送信
  3. 通知送信フラグを更新
- **エラーハンドリング**: 個別食品の通知失敗時も処理継続

### UI/UX 仕様

#### レスポンシブデザイン
- Bootstrap 5.3.2による レスポンシブレイアウト
- モバイルデバイス対応
- 外部アクセス対応（server.address=0.0.0.0）

#### 画面構成
1. **ログイン画面** (`login.html`)
2. **ユーザー登録画面** (`register.html`)
3. **食品一覧画面** (`list.html`) - メインダッシュボード
4. **食品追加画面** (`form.html`)
5. **食品編集画面** (`edit.html`)

#### デザイン特徴
- **大きなフォント**: 視認性重視のUI設計
- **統計カード**: Bootstrap カードコンポーネント使用
- **色分け表示**: 期限切れ・警告・通常の状態別色分け
- **操作ボタン**: 編集・削除の直感的配置

### セキュリティ仕様

#### 認証設定
```java
- フォームベース認証
- ログイン成功時: "/" へリダイレクト
- ログアウト成功時: "/login?logout" へリダイレクト
- 未認証アクセス時: "/login" へリダイレクト
```

#### 認可設定
```java
- "/login", "/register": 全ユーザーアクセス許可
- "/h2-console/**": 開発環境用H2コンソールアクセス
- その他全URL: 認証必須
```

### 開発・運用仕様

#### 環境要件
- **Java**: JDK 21以上
- **Maven**: 3.9.11以上
- **ブラウザ**: モダンブラウザ（Chrome, Firefox, Safari, Edge）

#### 起動方法
```bash
mvn spring-boot:run
```
- アクセスURL: `http://localhost:8080`
- H2コンソール: `http://localhost:8080/h2-console` (開発用)

#### ログ設定
- Spring Security: DEBUG レベル
- アプリケーション: DEBUG レベル
- メール送信: DEBUG レベル
- コンソール出力とファイル出力対応

---

## 開発環境セットアップ
