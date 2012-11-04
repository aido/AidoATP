#!/bin/bash +xv

declare BASE_DIR=~/Downloads/IsisATP

declare SRC_DIR=$BASE_DIR/src
declare BIN_DIR=$SRC_DIR/IsisATP/bin
declare CLASSPATH=$BASE_DIR/classes


[[ ! -d $BASE_DIR ]] && mkdir -p $BASE_DIR
[[ ! -d $SRC_DIR ]] && mkdir -p $SRC_DIR
[[ ! -d $BIN_DIR/bin ]] && mkdir -p $BIN_DIR
[[ -d $CLASSPATH ]] && rm -r $CLASSPATH
[[ ! -d $CLASSPATH ]] && mkdir -p $CLASSPATH

echo
echo "joda-convert"
echo "============"
[[ ! -d $SRC_DIR/joda-convert ]] && cd $SRC_DIR && git clone https://github.com/JodaOrg/joda-convert
cd $SRC_DIR/joda-convert
git pull
find $SRC_DIR/joda-convert -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
echo

echo "joda-money"
echo "============"
[[ ! -d $SRC_DIR/joda-money ]] && cd $SRC_DIR && git clone https://github.com/JodaOrg/joda-money
cd $SRC_DIR/joda-money
git pull
find $SRC_DIR/joda-money -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print| xargs javac -d $CLASSPATH -classpath $CLASSPATH
echo

echo "joda-time"
echo "============"
[[ ! -d $SRC_DIR/joda-time ]] && cd $SRC_DIR && git clone https://github.com/JodaOrg/joda-time
cd $SRC_DIR/joda-time
git pull
find $SRC_DIR/joda-time -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
[[ ! -d $CLASSPATH/org/joda/time/tz/data ]] && mkdir $CLASSPATH/org/joda/time/tz/data
java -classpath $CLASSPATH org.joda.time.tz.ZoneInfoCompiler -verbose -src $SRC_DIR/joda-time/src/main/java/org/joda/time/tz/src/ -dst $CLASSPATH/org/joda/time/tz/data africa antarctica asia australasia backward etcetera europe northamerica pacificnew southamerica systemv > /dev/null
echo -e "\n'Resource not found: \"org/joda/time/tz/data/ZoneInfoMap\"' error may be ignored if this is first time creating ZoneInfo"
echo

echo "slf4j-api"
echo "========="
[[ ! -d $SRC_DIR/slf4j ]] && cd $SRC_DIR && git clone https://github.com/qos-ch/slf4j
cd $SRC_DIR/slf4j
git pull
find $SRC_DIR/slf4j/slf4j-api -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
rm -r $CLASSPATH/org/slf4j/impl/
echo

# logback is pain in ass to build from source so do it the easy way
echo "logback"
echo "========"
cd $CLASSPATH
jar xvf $SRC_DIR/jars/logback-core-1.0.7.jar > /dev/null
jar xvf $SRC_DIR/jars/logback-classic-1.0.7.jar > /dev/null
echo

echo "JSON-java"
echo "========="
[[ ! -d $SRC_DIR/JSON-java ]] && cd $SRC_DIR && git clone https://github.com/douglascrockford/JSON-java
cd $SRC_DIR/JSON-java
git pull
find $SRC_DIR/JSON-java -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
echo

# commons-codec not needed for Xchange > 1.1.0
#echo "commons-codec"
#echo "============="
#[[ ! -d $SRC_DIR/commons-codec ]] && cd $SRC_DIR && git clone git://git.apache.org/commons-codec.git
#cd $SRC_DIR/commons-codec
#git pull
#find $SRC_DIR/commons-codec -type d -name *test* -prune -o -type d -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
#echo

echo "jackson"
echo "========"
[[ ! -d $SRC_DIR/jackson ]] && cd $SRC_DIR && git svn clone https://svn.codehaus.org/jackson/trunk jackson
cd $SRC_DIR/jackson
git svn rebase
find $SRC_DIR/jackson/src/java -type d -name *test* -prune -o -type d -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
find $SRC_DIR/jackson/src/mapper/java -type d -name *test* -prune -o -type d -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
echo

echo "Xchange"
echo "======="
[[ ! -d $SRC_DIR/XChange ]] && cd $SRC_DIR && git clone https://github.com/timmolter/XChange
cd $SRC_DIR/XChange
git pull
find $SRC_DIR/XChange -type d -name *test* -prune -o -name *example* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
# XChange 1.1.0
#cp $SRC_DIR/XChange/xchange/src/main/resources/org/joda/money/MoneyData.csv $CLASSPATH/org/joda/money/
# XChange 1.2.0
cp $SRC_DIR/XChange/xchange-core/src/main/resources/org/joda/money/MoneyData.csv $CLASSPATH/org/joda/money/
echo

echo "IsisATP"
echo "========"
[[ ! -d $SRC_DIR/IsisATP ]] && cd $SRC_DIR && git clone https://github.com/aido/IsisATP
find $SRC_DIR/IsisATP -type d -name *test* -prune -o -type f -name "*.java" -print | xargs javac -d $CLASSPATH -classpath $CLASSPATH
echo

echo "Building jar"
echo "============"
cd $CLASSPATH
jar -cfe $BIN_DIR/aido.jar org/open/payment/alliance/isis/atp/Application org com ch
echo

exit 0