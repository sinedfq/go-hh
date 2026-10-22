FROM golang:1.26.5-alpine AS build
ENV CGO_ENABLED=0
WORKDIR /src
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY cmd/api ./cmd/api
COPY internal ./internal
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build go build -trimpath -o /out/api ./cmd/api

FROM build AS goose-build
ARG GOOSE_VERSION=v3.24.3
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build go install -tags='no_clickhouse no_libsql no_mssql no_mysql no_sqlite3 no_vertica no_ydb' github.com/pressly/goose/v3/cmd/goose@${GOOSE_VERSION}

FROM alpine:3.22 AS runtime
RUN apk add --no-cache ca-certificates tzdata && addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app

FROM runtime AS migrations
COPY --from=goose-build /go/bin/goose /usr/local/bin/goose
COPY migrations /app/migrations
USER app
ENTRYPOINT ["goose", "-dir", "/app/migrations"]
CMD ["up"]

FROM runtime AS api
COPY --from=build /out/api /usr/local/bin/trudomix-api
COPY --chown=app:app uploads/demo_*.png /app/uploads/
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["trudomix-api"]
