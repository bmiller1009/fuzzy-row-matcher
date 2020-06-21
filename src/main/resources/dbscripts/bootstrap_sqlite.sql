CREATE TABLE json_data_**TIMESTAMP** (
	id	INTEGER NOT NULL,
	json_row	TEXT NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE scores_**TIMESTAMP** (
	id	INTEGER NOT NULL,
	json_data1_row_id	INTEGER NOT NULL,
	json_data2_row_id	INTEGER NOT NULL,
	jaro_dist_score	REAL NULL,
	levenshtein_distance_score	INTEGER NULL,
	hamming_distance_score	INTEGER NULL,
	jaccard_distance_score	REAL NULL,
	cosine_distance_score	REAL NULL,
	fuzzy_similiarity_score	INTEGER NULL,
	FOREIGN KEY(json_data1_row_id) REFERENCES json_data(id),
	FOREIGN KEY(json_data2_row_id) REFERENCES json_data(id),
	PRIMARY KEY(id,json_data1_row_id,json_data2_row_id)
);