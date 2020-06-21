# clsql
![](https://github.com/heyvito/clsql/workflows/Test/badge.svg)
[![cljdoc badge](https://cljdoc.org/badge/clsql/clsql)](https://cljdoc.org/d/clsql/clsql/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/clsql.svg)](https://clojars.org/clsql)

**clsql** provides a toolchain to work with databases and SQL for Clojure
applications.<br/>
It provides facilities to work with migrations and queries, allowing teams to
have reproducible migrations and easier to maintain queries, leaving SQL out
of Clojure.

## Reasoning
Clojure is a great tool to build DSLs, that's clear to every Clojure developer.
But maintaining raw SQL within Clojure can lead to cluttered code, and using
DSLs for such can make learning curves for new team members steeper, or
introduce hard-to-find bugs. For those reasons, **clsql** was built.

This library is heavily inspired by some of
[yesql](https://github.com/krisajenkins/yesql) features. It was built as a
side-project to train my (quite) rusty (E)BNF habilities.

## Installation

Add the following dependency to your `project.clj` file:

```clojure
[clsql "0.1.3"]
```

### Dependencies

**clsql** does not provide abstrations to SQL. It simply parses files in a
predetermined format, in a conventioned set of directories, and wraps contents
and annotations of those files into Clojure functions. In order to access a
database, you will need a **driver**:

|  Database  | Driver                                       |
|------------|----------------------------------------------|
| PostgreSQL | `[org.postgresql/postgresql "42.2.12.jre7"]` |

> **Notice**: Currently, I have only tested this library with PostgreSQL. In
case you use another database, feel free to test it and open a pull request to
update this table.


## Usage

As mentioned previouly, `clsql` handles both migrations and queries. Developers
are free to use only a single facility, or both of them.

### Initial configuration
Library defaults can be changed by changing
[`atom`](https://clojure.org/reference/atoms)s defined by the `clsql.config`
namespace:

| Atom                     | Required By | Description   |
|--------------------------|-------------|---------------|
| `migrations-directory`   | Migrations  | Indicates the directory in which the library will look for migration files. Defaults to `"resources/db/migrations"` |
| `database-configuration` | Migrations  | Defines configurations used by the migration facility to connect to the target database server. More information is available in the [Migrations](#migrations) section. |
| `queries-directory`      | Querying    | Indicates the directory in which the library will look for query files. Defaults to `"resources/db/queries"`|

### Migrations
In order to use any feature provided by the Migrations facility, your
application's entrypoint must be updated to give control over to `clsql` to
perform command-line parsing and execute any required operation. For such,
check the snippet below:

```clojure
(ns myapplication.core)

(defn- configure-clsql []
    (reset! clsql.config/migrations-directory "./another/directory"))

(defn -main []
  (configure-clsql)
  (when-not (clsql.cli-handler/handle-command-line-args)
    (comment Your Application initialisation procedures goes here)))
```

> **ProTip™**: Execute any modifications to the library's configuration options
prior to calling `handle-command-line-args`.

`handle-command-line-args` will return `false` in case it could not detect
any operation request. The following operations can be executed:

| Command | Description |
|---------|-------------|
| `create-migration NAME` | Creates a new migration stub in the configured `migrations-directory`. `NAME` can only contain letters (A-Za-z), numbers (0-9) and dashes (-) |
| `db-migrate` | Executes every pending migration |
| `db-rollback` | Rolls-back the database by executing the `down` section of the last migration. |
| `db-rollback VERSION` | Rolls-back the database by executing the `down` section of every migration created after `VERSION` |
| `db-migration-status` | Displays a table listing all migrations and their respective statuses |

#### Working with Migrations
Each migration created with `create-migration` is composed by two sections:
`up` and `down`.
`up` is required, and must contain every SQL command needed to migrate your
database to the next version.
`down` is optional. When defined, it must contain every SQL command needed to
revert changes made by `up`. When undefined, a migration will be considered
irreversible. Trying to rollback it will result in an error, indicating that
it cannot be reversed. Below you can find a migration example:

```sql
-- This creates a new `users` table.
-- Notice: Comments are allowed anywhere. The only thing clsql cares about
-- is region separators, indicated with `--;; <region>`, where `<region>`
-- may be `up` or `down`.

--;; up
CREATE TABLE users
    (id INTEGER NOT NULL PRIMARY KEY,
     name VARCHAR NOT NULL, -- We're allowing comments to be here as well.
     email TEXT NOT NULL UNIQUE);

--
-- This is another comment, between regions.
--

--;; down
DROP TABLE users;
```

#### Migration safety
Migrations are executed within a transaction. This way, in case anything goes
wrong, all changes are automatically reverted.

> **WARNING**: Not every RDBMS accepts schema modifications within transactions.

### Queries

Queries are defined in plain SQL files. Each file may contain an arbitrary number
of queries. Each query must contain at least a name, but can also include
additional information, such as a docstring, and modifiers.

#### Defining queries

To define queries, first ensure the directory configured by
[`clsql.config/queries-directory`](#initial-configuration) exists. Then, create
a new file, for examples here, assume a file named `users.sql`. Then, define a
new query by prefixing it with a name:

```sql
--> active-users
```

Names are indicated by the prefix `-->`, followed by how one wants to reference
the query on Clojure's side. In the example above, we're creating an
`active-users` query. Let's add some documentation to it:

```sql
--> active-users
-- Returns all users marked as active.
-- Users are marked as active after confirming their email addresses.
```

The code above will allow the library to automatically include documentation
to generated functions, making them available through [`doc`](https://clojuredocs.org/clojure.repl/doc).

Then, write your SQL as you would:

```sql
--> active-users
-- Returns all users marked as active.
-- Users are marked as active after confirming their email addresses.
SELECT *
FROM users
WHERE activated_at IS NOT NULL
```

#### Importing queries

After configuring and defining your SQL, simply import it by using
`require-query` or `require-queries`:

```clojure
(ns myapplication.some-namespace
    :require [clsql.core :refer [require-query]])

(require-query users :refer [active-users])

(defn list-active-users []
    (active-users db-spec nil))
```

The result will be a list of maps of all returned results.

In the REPL, documentation can be also accessed:

```clojure
=> (doc active-users)
-------------------------
myapplication.some-namespace/active-users
([db args & opts])
  Returns all users marked as active. Users are marked as active after
  confirming their email addresses.
```

#### Queries with placeholders

**clsql** has support to placeholders. Defining them in your SQL allows
generated functions to safely replace them into generated queries, and also
to perform validations on whether required parameters are provided when invoking
such functions. Let's keep working on our `users.sql` file by adding a new
query:

```sql
--> activated-after
-- Returns all users activated after a given date.
SELECT *
FROM users
WHERE activated_at >= :date
```

The query above declares a placeholder by using the `:NAME` notation. Let's
import it and see what happens:

```clojure
(require-query users :as users)

; require-query accepts :as and :refer, just like `require`. For more
; information, see require-query and require-queries documentation.
```

Now, let's try to invoke it without arguments and see how it behaves:

```clojure
=> (users/activated-after db nil)
Execution error (IllegalArgumentException) at ...
Missing parameter(s): :date
```

The generated function knows a placeholder was defined, but not provided during
the invocation. Thus, an `IllegalArgumentException` was thrown. To fix it, let's
provide what it needs:

```clojure
=> (users/activated-after db {:date "2020-01-24"})
...
```

> **ProTip™**: Wondering what `db` is in all those calls? Wonder no more! `db`
is just a db-spec structure just like one would use with
[`clojure.java.jdbc`](https://github.com/clojure/java.jdbc). It can also be a
transaction, obtained from [`with-db-transaction`](http://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql.html#using-transactions).

## Contributions
...are more than welcome! <br/>
Feel free to fork and open a pull request. Please
include tests, and ensure all tests are green!

## License

```
MIT License

Copyright (c) 2020 - Victor Gama de Oliveira

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
