FROM azul/zulu-openjdk:8
MAINTAINER Shisei Hanai<ruimo.uno@gmail.com>

RUN apt-get update
RUN apt-get install unzip
RUN useradd -d "/var/home" -s /bin/bash fsuser
RUN mkdir -p /opt/fs
ADD target/universal/*.zip /opt/fs

RUN cd /opt/fs && \
  cmd=$(basename *.zip .zip) && \
  unzip -q $cmd.zip

RUN cd /opt/fs && \
  cmd=$(basename *.zip .zip) && \
  echo "#!/bin/bash -e" > launch.sh && \
  echo printenv >> launch.sh && \
  echo "ls -lh /opt/fs" >> launch.sh && \
  echo 'kill -9 `cat /opt/fs/$cmd/RUNNING_PID`' && \
  echo rm -f /opt/fs/$cmd/RUNNING_PID >> launch.sh && \
  echo /opt/fs/$cmd/bin/fs -J-Xmx512m -Dplay.crypto.secret='${APP_SECRET}' -Dplay.evolutions.db.default.autoApply=true -Dconfig.resource=\${CONF_FILE} >> launch.sh && \
  chmod +x launch.sh

RUN chown -R fsuser:fsuser /opt/fs
USER fsuser

EXPOSE 3000

ENTRYPOINT ["/bin/bash", "-c", "/opt/fs/launch.sh"]
