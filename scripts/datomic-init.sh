echo "PGUSER ${PGUSER}"
echo "PGHOST ${PGHOST}"
echo "PGPASSWORD ${PGPASSWORD}"

if [ "$( psql -XtAc "SELECT 1 FROM pg_database WHERE datname='datomic'" )" = '1' ]
then
	echo "datomic database already exists"
else
	echo "creating the datomic database..."
	psql --file=./scripts/postgres-db.sql && \
		psql --file=./scripts/postgres-table.sql
fi
