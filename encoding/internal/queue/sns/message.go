package sns

type Message struct {
	VideoId    string `json:"videoId"`
	Resolution string `json:"resolution"`
	ChunkName  string `json:"chunkName"`
}
