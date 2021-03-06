#!/bin/bash
#-------------------------------------------------------------
#
# (C) Copyright IBM Corp. 2010, 2015
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#-------------------------------------------------------------

if [ "$5" == "SPARK" ]; then CMD="./sparkDML.sh "; DASH="-"; elif [ "$5" == "MR" ]; then CMD="hadoop jar SystemML.jar " ; else CMD="echo " ; fi

BASE=$4

export HADOOP_CLIENT_OPTS="-Xmx2048m -Xms2048m -Xmn256m"

#for all intercept values
for i in 0 1
do
   #training
   tstart=$SECONDS
   ${CMD} -f ../algorithms/m-svm.dml $DASH-explain $DASH-stats $DASH-nvargs X=$1 Y=$2 icpt=$i classes=$3 tol=0.0001 reg=0.01 maxiter=3 model=${BASE}/w Log=${BASE}/debug_output fmt="csv"
   ttrain=$(($SECONDS - $tstart - 3))
   echo "MSVM train ict="$i" on "$1": "$ttrain >> times.txt

   #predict
   tstart=$SECONDS
   ${CMD} -f ../algorithms/m-svm-predict.dml $DASH-explain $DASH-stats $DASH-nvargs X=$1_test Y=$2_test icpt=$i model=${BASE}/w fmt="csv"
   tpredict=$(($SECONDS - $tstart - 3))
   echo "MSVM predict ict="$i" on "$1": "$tpredict >> times.txt
done