# book-manager

書籍とその著者を管理するアプリケーション。DDD（domain/application/infrastructure/presentation）構成のSpring Boot + Kotlin + jOOQ + Flyway製REST API。

API仕様書は[こちら](https://github.com/Y-248/book-manager/issues/2#issuecomment-5014823041)

DB仕様書は[こちら](https://github.com/Y-248/book-manager/issues/1#issuecomment-5014721122)

## API一覧

| メソッド | パス | 概要 |
|---|---|---|
| POST | `/api/v1/authors` | 著者の登録 |
| PATCH | `/api/v1/authors/{authorId}` | 著者の更新 |
| POST | `/api/v1/books` | 書籍の登録（著者の登録／紐付けを同時に行える） |
| PATCH | `/api/v1/books/{bookId}` | 書籍の更新（著者紐付けの完全置換を含む） |
| GET | `/api/v1/books?authorId={authorId}` | 著者に紐づく書籍の取得 |

## 前提条件

- JDK 21（`JAVA_HOME`が別バージョンを指していないか要確認。環境によっては別途インストールされた古いJDKがデフォルトになっていることがある）
- Docker（WSL環境の場合はDockerデーモンが起動していること。`docker compose up -d`でPostgreSQLコンテナを起動する）

## jOOQコード生成

Flywayで適用したDBスキーマを実際に読み取って jOOQ のコードを生成する構成になっている。生成物は `build/generated-sources/jooq/` 配下に出力される。

Gradleのタスク依存により、`flywayMigrate` → `jooqCodegen` → `compileKotlin` の順に自動的に連鎖する。ただし `flywayMigrate`/`jooqCodegen` はGradleタスクとして直接JDBCで `localhost:5432` に接続するため、**実行前にPostgreSQLコンテナが起動している必要がある**（アプリ起動時のDocker Compose自動連携より前のタイミングで動くため）。

### パターン1: `bootRun`するだけ（コンテナが起動済みの場合）

前回の作業などで既にPostgreSQLコンテナが起動している場合は、これだけで完結する。

```bash
./gradlew bootRun
```

実行すると、Gradleのタスク依存によって以下が自動的に順番に走る。

1. `flywayMigrate` — 未適用のマイグレーションを実DBに適用
2. `jooqCodegen` — 適用後のスキーマからKotlinコードを生成
3. `compileKotlin` — 生成コードを含めてコンパイル
4. アプリ起動（Spring Boot自身のDocker Compose連携・Flyway自動マイグレーションも実行される）

> **注意**: コンテナが1つも起動していない状態でいきなり`bootRun`を叩くと、`flywayMigrate`がDBに接続できず失敗する。Spring Bootの自動Docker起動はアプリ本体が起動して初めて働くため、それより前に実行される`flywayMigrate`には間に合わない。コンテナが起動しているか不安な場合はパターン2を使う。

### パターン2: コンテナを明示的に起動してからマイグレーション・コード生成を行う（初回・コンテナが落ちている場合）

```bash
# 1. PostgreSQLコンテナを起動
docker compose up -d

# 2. マイグレーション適用 + コード生成（bootRunせずにコード生成だけ確認したい場合）
./gradlew jooqCodegen

# 3. アプリを起動
./gradlew bootRun
```

`jooqCodegen`を明示的に叩かなくても`bootRun`や`build`が自動的に前段のタスクを実行するが、コード生成だけを先に確認したい場合や、マイグレーションファイルを追加・変更した直後に生成結果だけ見たい場合はこの手順が便利。

### 生成物

- 出力先: `build/generated-sources/jooq/`
- パッケージ: `com.github.y248.book_manager.jooq`
- `flyway_schema_history`テーブルはコード生成対象から除外している

### 接続情報

`flywayMigrate`・`jooqCodegen`が使う接続情報は `build.gradle` 内に直書きしている（`compose.yaml`のPostgreSQLサービスと同じ値、ローカル開発専用のダミー認証情報）。

| 項目 | 値 |
|---|---|
| URL | `jdbc:postgresql://localhost:5432/mydatabase` |
| ユーザー | `myuser` |
| パスワード | `secret` |

## テスト

```bash
./gradlew test
```

`@WebMvcTest`を使うテストはDB不要だが、`test`タスク自体は`flywayMigrate`/`jooqCodegen`に依存しているため、実行前にPostgreSQLコンテナを起動しておく必要がある（[jOOQコード生成](#jooqコード生成)を参照）。

各テストの`@DisplayName`と結果（PASSED/FAILED）はターミナルにも出力されるが、**ソースに変更が無いとGradleが`UP-TO-DATE`と判断してテストの実行自体をスキップする**（結果として何もログが出ない）。前回と同じ内容でも強制的に再実行してログを見たい場合は、`--rerun`を付ける。

```bash
./gradlew test --rerun
```