#!/bin/zsh

# Start docker compose if it's not running already
if docker compose ls | grep multi-money
then
	echo "docker compose is already running"
else
	docker compose up -d
fi

session=multi-money

tmux new-session -d -s $session

# REPL window
tmux rename-window -t 0 'repl'
tmux send-keys 'clear' C-m 'lein repl' C-m

tmux split-window -v
tmux send-keys 'lein fig:build' C-m

# Code window
tmux new-window -t $session:1 -n $session
tmux send-keys 'nvim' C-m
tmux split-window -h
tmux send-keys 'git status' C-m

# SQL window
# Note that you need a file at ~/.pgpass with perms 0600 and content like this following:
# hostname:port:database:username:password
tmux new-window -t $session:2 -n 'sql' 'PGPASSWORD=please01 psql --host=localhost --username=adm_user -d multi_money_development --no-password'

# MongoDB window
tmux new-window -t $session:3 -n 'mongodb' 'mongosh mongodb://adm_user:please01@localhost:27017/admin'

# Log window
tmux new-window -t $session:4 -n 'logs'
tmux send-keys 'tail -f logs/development.log | grep -e ERROR -e WARN -e dbk' C-m
tmux split-window -v
tmux send-keys 'tail -f logs/development.log' C-m

# Kubernetes
tmux new-window -t $session:5 -n 'k8s'
tmux send-keys 'cd ../k8s' C-m
tmux send-keys 'nvim' C-m
tmux split-window -h
tmux send-keys 'cd ../k8s' C-m

tmux attach -t $session:1
