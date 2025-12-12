# 1. ビルド環境: Java 21 と Maven を使う
FROM maven:3.9-eclipse-temurin-21 AS build

# 作業ディレクトリ設定
WORKDIR /app

# プロジェクトのファイルをコピー
COPY . .

# Mavenでビルド（テストはスキップして高速化）
RUN mvn clean package -DskipTests

# ---------------------------------------------------

# 2. 実行環境: Java 21 (JDK) を使う
FROM eclipse-temurin:21-jdk-jammy

# ビルド成果物(jar)をコピー
COPY --from=build /app/target/*.jar app.jar

# ポート8080を開ける
EXPOSE 8080

# アプリ起動
ENTRYPOINT ["java", "-jar", "app.jar"]