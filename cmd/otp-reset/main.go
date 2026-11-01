package main

import (
	"context"
	"flag"
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sinedfq/go-hh/internal/phoneotp"
	"log"
	"os"
	"time"
)

func main() {
	phone := flag.String("phone", "", "phone in international format")
	all := flag.Bool("all", false, "reset all OTP limits, development only")
	flag.Parse()
	if (*phone == "" && !*all) || (*phone != "" && *all) {
		log.Fatal("Specify --phone or --all")
	}
	if *all && os.Getenv("APP_ENV") != "development" {
		log.Fatal("--all requires APP_ENV=development")
	}
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://postgres:postgres@localhost:5432/gohh?sslmode=disable"
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		log.Fatal("Cannot connect to database")
	}
	defer pool.Close()
	count, err := phoneotp.ResetLimits(ctx, pool, *phone, *all)
	if err != nil {
		log.Fatal("Cannot reset OTP limits: ", err)
	}
	fmt.Printf("OTP limits reset. Invalidated challenges: %d\n", count)
}
