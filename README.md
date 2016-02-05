[![Stories in Ready](https://badge.waffle.io/mojotech/modernator.png?label=ready&title=Ready)](https://waffle.io/mojotech/modernator)
# Modernator

# development dependencies

  * [postgres](http://postgresapp.com/)
  * [leiningen](http://leiningen.org/)
  * [forego](https://github.com/ddollar/forego) `brew install forego`

# database

  * `createdb modernator`
  * `lein ragtime migrate`

# local dev

  * `forego start`
  * navigate to http://localhost:8080

### Environment

To add or configure environment variables differently than [the default](src/clj/modernator/config.clj), simply copy the `.lein-env.sample` file and name it `.lein-env`.

# deploy

  * Set up box with [postgres](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-14-04)
    and [java](https://www.digitalocean.com/community/tutorials/how-to-install-java-on-ubuntu-with-apt-get) (just jre)
    * Create postgres user
  * Run migrations from migrations folder (still up in the air about the best way, I just scp-ed the files up and ran them)
  * Create uberjar from project (`lein uberjar`)
  * Upload standalone jar to box
  * Set appropriate env vars
    * `export MODERNATOR_URL=http://<domain_or_ip>:8080/`
    * `export DATABASE_URL=postgres://user:password@localhost:5432/modernator`
    * `export SMTP_HOST=`
    * `export SMTP_USER=`
    * `export SMTP_PASS=`
  * `java -jar <the_jar>`
  * See the fruit of your labor at http://<domain_or_ip>:8080

Copyright © 2015 MojoTech
