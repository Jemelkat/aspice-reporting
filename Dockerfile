#### Stage 1: Build the application
FROM adoptopenjdk/openjdk16:alpine as build

# Working directory
WORKDIR /app

# Copy maven executable to the image
COPY mvnw .
#Conversion of windows end of lines to unix
RUN apk add dos2unix
RUN dos2unix mvnw

COPY .mvn .mvn
COPY pom.xml .

# Build all the dependencies
RUN ./mvnw dependency:go-offline -B

# Copy the project source
COPY src src

# Package the application
RUN ./mvnw package -D skipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)


#### Stage 2: A minimal docker image
FROM adoptopenjdk/openjdk16:jre-16.0.1_9-alpine

ARG DEPENDENCY=/app/target/dependency

# Copy project dependencies from the build stage
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Install required fonts
RUN apk add --no-cache fontconfig
RUN apk add --no-cache ttf-dejavu
RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

ENTRYPOINT ["java","-cp","app:app/lib/*","com.aspicereporting.AspiceReportingApplication"]