if [ "$( PGPASSWORD=$SQL_ADM_PASSWORD psql --host=$SQL_HOST --user=$SQL_ADM_USER -XtAc "SELECT 1 FROM pg_database WHERE datname='datomic'" )" = '1' ]
then
	echo "datomic database already exists"
else
	echo "creating the datomic database..."
	PGPASSWORD=$SQL_ADM_PASSWORD psql \
		--file=./scripts/postgres-db.sql \
		--username=$SQL_ADM_USER \
		--host=$SQL_HOST
	PGPASSWORD=$SQL_ADM_PASSWORD psql \
		--file=./scripts/postgres-table.sql \
		--username=$SQL_ADM_USER \
		--host=$SQL_HOST \
		--dbname=datomic
fi
