FROM golang:1.25.5-alpine AS builder

WORKDIR /app

COPY go.mod ./
COPY go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o main cmd/worker/main.go

FROM alpine:3.19 AS final

RUN apk --no-cache add ca-certificates ffmpeg

WORKDIR /root/

COPY --from=builder /app/main .

CMD ["./main"]