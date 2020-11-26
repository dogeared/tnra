#! /bin/bash

TOKEN=$1
URL=$2

function doIt {
  http -f POST ${URL}/api/v1/post \
  token=${TOKEN} \
  team_id=T1DLRUNN8 \
  team_domain=tnra \
  channel_id=D1DN187CH \
  channel_name=directmessage \
  user_id=U1DMD2Z8A \
  user_name=afitnerd \
  response_url=https://hooks.slack.com/commands/T1DLRUNN8/1522759899702/WNXLruMwCJKUT9VhBODbIZwP \
  trigger_id=1553333092576.47705974756.7e452cdd0aab5a165b1b7dfdd3f9dba5 \
  command=/post_test \
  text="$1"
  sleep 1
}

doIt start
doIt "rep int wid wid test"
doIt "rep int kry kry test"
doIt "rep int wha wha test"
doIt "rep per bes bes test"
doIt "rep per wor wor test"
doIt "rep fam bes bes test"
doIt "rep fam wor wor test"
doIt "rep wor bes bes test"
doIt "rep wor wor wor test"
doIt "rep sta exe:0 gtg:0 med:0 mee:0 pra:0 rea:0 spo:0"
doIt finish