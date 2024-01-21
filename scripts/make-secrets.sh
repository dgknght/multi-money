kubectl create secret generic secret-config -n $1 \
	--from-file=./config/integration/APP_SECRET \
	--from-file=./config/integration/DB \
	--from-file=./config/integration/GOOGLE_OAUTH_CLIENT_ID \
	--from-file=./config/integration/GOOGLE_OAUTH_CLIENT_SECRET
