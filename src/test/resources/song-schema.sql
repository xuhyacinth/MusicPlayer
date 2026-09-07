CREATE TABLE song (
    `id` varchar (50) primary key, 

    `name` varchar (500) default null, 
    `info` varchar (1000) default null,  
    `flag` integer (1) default (0),
    `index` int (5) default (0),

    `author` varchar (100) default null,   
    `length` double (6) default (0.0), 
    `song_path` varchar (500) default null,

    `lyric_info` text default null,   
    `lyric_path` varchar (500) default null,   

    `create_by` varchar (50) default null,  
    `create_time` datetime default null, 
    `update_by` varchar (50) default null,  
    `update_time` datetime default null
)