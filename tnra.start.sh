#! /bin/bash

cd ~/tnra

source ./.env

docker-compose up --build --detach
