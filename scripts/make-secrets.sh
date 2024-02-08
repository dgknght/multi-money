# When creating the files below, be careful not to allow a new line character to
# be inserted at the end of the file. E.g., to create APP_SECRET, try
# echo -n this-is-the-app-secret > config/integration/APP_SECRET
kubectl create secret generic secret-config -n $1 \
	--from-file=./config/integration/APP_SECRET \
	--from-file=./config/integration/DB \
	--from-file=./config/integration/GOOGLE_OAUTH_CLIENT_ID \
	--from-file=./config/integration/GOOGLE_OAUTH_CLIENT_SECRET \
	--from-file=./config/integration/SQL_ADM_USER \
	--from-file=./config/integration/SQL_ADM_PASSWORD \
	--from-file=./config/integration/SQL_APP_USER \
	--from-file=./config/integration/SQL_APP_PASSWORD \
	--from-file=./config/integration/SQL_DDL_USER \
	--from-file=./config/integration/SQL_DDL_PASSWORD
