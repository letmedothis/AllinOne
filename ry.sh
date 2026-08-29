#!/bin/bash
# ./ry.sh start 启动 stop 停止 restart 重启 status 状态
AppName=allinone-admin.jar

# JVM参数
JVM_OPTS="-Dname=$AppName  -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -Xlog:gc*:logs/gc.log:time,uptime:filecount=5,filesize=10m"
APP_HOME=`pwd`
LOG_PATH=$APP_HOME/logs/$AppName.log
# logback 的 sys-info/sys-error/sys-user 日志写到应用目录，便于排障与回收
JVM_OPTS="$JVM_OPTS -DLOG_PATH=$APP_HOME/logs"

if [ "$1" = "" ];
then
    echo -e "\033[0;31m 未输入操作名 \033[0m  \033[0;34m {start|stop|restart|status} \033[0m"
    exit 1
fi

if [ "$AppName" = "" ];
then
    echo -e "\033[0;31m 未输入应用名 \033[0m"
    exit 1
fi

start()
{
    PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`

	if [ x"$PID" != x"" ]; then
	    echo "$AppName is running..."
	else
		mkdir -p "$APP_HOME/logs"
		nohup java $JVM_OPTS -jar $AppName > "$APP_HOME/logs/startup.out" 2>&1 &
		echo "Start $AppName success..."
	fi
}

stop()
{
    echo "Stop $AppName"

	PID=""
	query(){
		PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`
	}

	query
	if [ x"$PID" != x"" ]; then
		kill -TERM $PID
		echo "$AppName (pid:$PID) exiting..."
		# 最多等 30 秒，超时强制 kill，避免 stop 卡死
		waited=0
		while [ x"$PID" != x"" ] && [ "$waited" -lt 30 ]
		do
			sleep 1
			waited=$((waited + 1))
			query
		done
		if [ x"$PID" != x"" ]; then
			kill -9 $PID
			echo "$AppName force killed after 30s."
		else
			echo "$AppName exited."
		fi
	else
		echo "$AppName already stopped."
	fi
}

restart()
{
    stop
    sleep 2
    start
}

status()
{
    PID=`ps -ef |grep java|grep $AppName|grep -v grep|wc -l`
    if [ $PID != 0 ];then
        echo "$AppName is running..."
    else
        echo "$AppName is not running..."
    fi
}

case $1 in
    start)
    start;;
    stop)
    stop;;
    restart)
    restart;;
    status)
    status;;
    *)

esac
