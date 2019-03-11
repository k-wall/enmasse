FROM centos:7

RUN mkdir -p /opt/console/go/src/github.com/enmasseproject/enmasse/console
WORKDIR /opt/console/go/src/github.com/enmasseproject/enmasse/console

RUN yum -y update
RUN yum -y install epel-release --enablerepo=extras
RUN yum -y install nodejs golang

ENV \
    GOPATH=/opt/console/go

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/console-${MAVEN_VERSION}-dist.tar.gz /opt/console/go/src/github.com/enmasseproject/enmasse/console

RUN GO111MODULE=on go mod vendor

EXPOSE 8080

CMD ["go", "run", "/opt/console/go/src/github.com/enmasseproject/enmasse/console/cmd/main.go"]


