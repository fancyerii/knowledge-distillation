#!/bin/bash
source common.sh
echo $MY_ES_SERVER
curl -XDELETE "http://${MY_ES_SERVER}/fo/" 
curl  -s -XPOST "http://${MY_ES_SERVER}/fo/" -d@$(dirname $0)/mapping.json
