-- Sample music data for stories.
-- This file is NOT a Flyway migration. Replace audio_url/cover_url with real URLs before running.
-- run: psql -h localhost -p 5432 -U your_username -d your_database -f src/main/resources/db/seed/insert_music_tracks_sample.sql
insert into music_tracks (
    title,
    artist,
    audio_url,
    cover_url,
    duration_seconds,
    is_active
) values
(
    'Vô giá',
    'Minh Vương',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156489/qofzkngzu0mkxokm4slw.mp3',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156489/qofzkngzu0mkxokm4slw.mp3',
    200,
    true
),
(
    'Trên tình bạn dưới tình yêu',
    'Min',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156487/lsxzyj5hs4iflha1337i.mp3',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156487/lsxzyj5hs4iflha1337i.mp3',
    200,
    true
),
(
    'Hẹn ước xin khuất lời',
    'Quốc Thiên',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156480/jqhv5q9aoc9g79fy495r.mp3',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156480/jqhv5q9aoc9g79fy495r.mp3',
    195,
    true
),
(
    'Em mang đi mùa hạ',
    'Gấu ấm áp',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156479/phoyo9kwcqc18glxamzw.mp3',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156479/phoyo9kwcqc18glxamzw.mp3',
    168,
    true
),
(
    'Gặp anh giữa luân hồi',
    'Mai Xuân Thứ',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156478/ls4pgnsrqzbsnfau47c5.mp3',
    'https://res.cloudinary.com/cybersocial/video/upload/v1780156478/ls4pgnsrqzbsnfau47c5.mp3',
    240,
    true
);
