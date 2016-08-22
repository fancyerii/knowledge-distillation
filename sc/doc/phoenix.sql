create table baidumusic.unfinished (
	failcount INTEGER not null,
	priority INTEGER not null,
	enqueue_time BIGINT not null,
	id BIGINT,
	url varchar,
	depth TINYINT,
	lastVisit BIGINT,
	redirected_url varchar,
	CONSTRAINT pk PRIMARY KEY (failcount, priority desc, enqueue_time) 
);

CREATE INDEX baidumusic_idx ON baidumusic.unfinished (id) ;