CREATE TABLE json_data_**TIMESTAMP** (
	id	TEXT NOT NULL,
	json_row	TEXT NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE scores_**TIMESTAMP** (
	id	TEXT NOT NULL,
	json_data1_row_id	TEXT NOT NULL,
	json_data2_row_id	TEXT NOT NULL,
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

CREATE VIEW final_scores_**TIMESTAMP** AS
SELECT a.json_row json_row_1, c.json_row json_row_2,
b.jaro_dist_score,
	b.levenshtein_distance_score	,
	b.hamming_distance_score	,
	b.jaccard_distance_score	,
	b.cosine_distance_score	,
	b.fuzzy_similiarity_score
FROM
json_data_**TIMESTAMP** a
INNER JOIN scores_**TIMESTAMP** b ON a.id = b.json_data1_row_id
INNER JOIN json_data_**TIMESTAMP** c ON c.id = b.json_data2_row_id;