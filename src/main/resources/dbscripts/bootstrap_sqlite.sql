CREATE TABLE json_data_**TIMESTAMP** (
	id	INTEGER,
	json_row	TEXT,
	PRIMARY KEY(id)
);

CREATE TABLE scores_**TIMESTAMP** (
	id	INTEGER,
	json_data1_row_id	INTEGER,
	json_data2_row_id	INTEGER,
	jaro_dist_score	REAL,
	levenshtein_distance_score	INTEGER,
	hamming_distance_score	INTEGER,
	jaccard_distance_score	REAL,
	cosine_distance_score	REAL,
	fuzzy_similiarity_score	INTEGER,
	FOREIGN KEY(json_data1_row_id) REFERENCES json_data(id),
	FOREIGN KEY(json_data2_row_id) REFERENCES json_data(id),
	PRIMARY KEY(id,json_data1_row_id,json_data2_row_id)
);