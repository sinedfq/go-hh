package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
)

type GeocodeResponse struct {
	Address   string  `json:"address"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

func (s *Server) geocodeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	address := r.URL.Query().Get("address")
	if address == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "address required"})
		return
	}

	// Получаем ключ из окружения или используем константу
	apiKey := os.Getenv("YANDEX_GEOCODER_API_KEY")
	if apiKey == "" {
		apiKey = "2878ed4b-0ffb-4a5c-b4eb-79b1a1fc641d"
	}

	// Запрос к Яндекс Geocoder API
	geocodeURL := fmt.Sprintf(
		"https://geocode-maps.yandex.ru/1.x/?apikey=%s&geocode=%s&format=json&results=1",
		apiKey,
		url.QueryEscape(address),
	)

	resp, err := http.Get(geocodeURL)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "geocode request failed"})
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		w.WriteHeader(resp.StatusCode)
		json.NewEncoder(w).Encode(map[string]string{
			"error": fmt.Sprintf("geocoder returned %d: %s", resp.StatusCode, string(body)),
		})
		return
	}

	var result struct {
		Response struct {
			GeoObjectCollection struct {
				FeatureMember []struct {
					GeoObject struct {
						Point struct {
							Pos string `json:"pos"`
						} `json:"Point"`
						MetaDataProperty struct {
							GeocoderMetaData struct {
								Text string `json:"text"`
							} `json:"GeocoderMetaData"`
						} `json:"metaDataProperty"`
					} `json:"GeoObject"`
				} `json:"featureMember"`
			} `json:"GeoObjectCollection"`
		} `json:"response"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to parse geocoder response"})
		return
	}

	if len(result.Response.GeoObjectCollection.FeatureMember) == 0 {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "address not found"})
		return
	}

	feature := result.Response.GeoObjectCollection.FeatureMember[0]
	pos := feature.GeoObject.Point.Pos

	var lng, lat float64
	fmt.Sscanf(pos, "%f %f", &lng, &lat)

	json.NewEncoder(w).Encode(GeocodeResponse{
		Address:   feature.GeoObject.MetaDataProperty.GeocoderMetaData.Text,
		Latitude:  lat,
		Longitude: lng,
	})
}

// ====== ПОДСКАЗКИ (множественные результаты) ======
func (s *Server) geocodeSuggestHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	address := r.URL.Query().Get("address")
	if address == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "address required"})
		return
	}

	apiKey := os.Getenv("YANDEX_GEOCODER_API_KEY")
	if apiKey == "" {
		apiKey = "2878ed4b-0ffb-4a5c-b4eb-79b1a1fc641d"
	}

	geocodeURL := fmt.Sprintf(
		"https://geocode-maps.yandex.ru/1.x/?apikey=%s&geocode=%s&format=json&results=5",
		apiKey,
		url.QueryEscape(address),
	)

	resp, err := http.Get(geocodeURL)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "geocode request failed"})
		return
	}
	defer resp.Body.Close()

	var result struct {
		Response struct {
			GeoObjectCollection struct {
				FeatureMember []struct {
					GeoObject struct {
						Point struct {
							Pos string `json:"pos"`
						} `json:"Point"`
						MetaDataProperty struct {
							GeocoderMetaData struct {
								Text string `json:"text"`
							} `json:"GeocoderMetaData"`
						} `json:"metaDataProperty"`
					} `json:"GeoObject"`
				} `json:"featureMember"`
			} `json:"GeoObjectCollection"`
		} `json:"response"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		json.NewEncoder(w).Encode([]GeocodeResponse{})
		return
	}

	var suggestions []GeocodeResponse
	for _, feature := range result.Response.GeoObjectCollection.FeatureMember {
		pos := feature.GeoObject.Point.Pos
		var lng, lat float64
		fmt.Sscanf(pos, "%f %f", &lng, &lat)

		suggestions = append(suggestions, GeocodeResponse{
			Address:   feature.GeoObject.MetaDataProperty.GeocoderMetaData.Text,
			Latitude:  lat,
			Longitude: lng,
		})
	}

	if suggestions == nil {
		suggestions = []GeocodeResponse{}
	}

	json.NewEncoder(w).Encode(suggestions)
}
