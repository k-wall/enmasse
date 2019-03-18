FROM keithwall/oauth-proxy:latest

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/console-${MAVEN_VERSION}-dist.tar.gz /

