-- ============================================================
-- V13 球星照片：用真实球星照片填充 AI 预测页右侧
-- ------------------------------------------------------------
-- 桌面 "famous player" 目录下的 20 张照片，已复制到前端
-- public/star-players/<team-en-slug>.jpg（每队 1 张；文件名已清洗为 ASCII，
-- 避免 URL 里出现 & / 空格 / 重音符号导致加载失败）。
--
-- 这里把 team_star_player 从 V8 占位（仅 6 队、无照片、球员为演示数据）
-- 替换为 20 队真实球星：player_name 取自照片文件名，photo_url 指向前端静态资源。
--
-- 照片作为【前端静态资源】随 public/ 走，后端只存 URL 字符串：
--   开发态 Vite 从 public/ 提供 /star-players/*（.ti-stars 不依赖后端静态服务）；
--   构建态 public/ 被原样拷进 dist/，路径同样有效。
--
-- team_name 必须与 team_prediction.team_name 完全一致（PredictionService 按队名回连），
-- 故托特纳姆/利兹联/考文垂/桑德兰/赫尔城用 V11 的写法，而非 V8 字典里的别名。
--
-- 兼容性：DELETE + INSERT，仅用 MySQL 8 与 H2(MySQL 模式) 都支持的语法。
-- ============================================================

DELETE FROM team_star_player;

INSERT INTO team_star_player (team_name, player_name, position, jersey_number, photo_url, sort_order) VALUES
('阿森纳',       'Declan Rice',        '后腰',   41, '/star-players/arsenal.jpg',              1),
('曼城',         'Erling Haaland',     '中锋',    9, '/star-players/manchester-city.jpg',      1),
('利物浦',       'Virgil van Dijk',    '中卫',    4, '/star-players/liverpool.jpg',            1),
('曼联',         'Bruno Fernandes',    '前腰',    8, '/star-players/manchester-united.jpg',    1),
('切尔西',       'Cole Palmer',        '前腰',   20, '/star-players/chelsea.jpg',              1),
('阿斯顿维拉',   'John McGinn',        '中场',    7, '/star-players/aston-villa.jpg',          1),
('托特纳姆热刺', 'Sandro Tonali',      '中场',    8, '/star-players/tottenham-hotspur.jpg',    1),
('布莱顿',       'Pascal Gross',       '中场',   13, '/star-players/brighton-hove-albion.jpg', 1),
('纽卡斯尔联',   'Nico González',      '中场',   23, '/star-players/newcastle-united.jpg',     1),
('布伦特福德',   'Igor Thiago',        '中锋',    9, '/star-players/brentford.jpg',            1),
('利兹联',       'Daniel James',       '边锋',   18, '/star-players/leeds-united.jpg',         1),
('富勒姆',       'Oscar Bobb',         '边锋',   11, '/star-players/fulham.jpg',               1),
('水晶宫',       'Dean Henderson',     '门将',    1, '/star-players/crystal-palace.jpg',       1),
('伯恩茅斯',     'Alex Scott',         '中场',   10, '/star-players/afc-bournemouth.jpg',      1),
('埃弗顿',       'Jordan Pickford',    '门将',    1, '/star-players/everton.jpg',              1),
('诺丁汉森林',   'Morgan Gibbs-White', '前腰',   10, '/star-players/nottingham-forest.jpg',    1),
('考文垂',       'Matt Grimes',        '中场',    6, '/star-players/coventry-city.jpg',        1),
('桑德兰',       'Granit Xhaka',       '中场',   34, '/star-players/sunderland.jpg',           1),
('赫尔城',       'Konstantinos Tzolakis','门将', 1, '/star-players/hull-city.jpg',            1),
('伊普斯维奇',   'Exequiel Palacios',  '中场',    5, '/star-players/ipswich-town.jpg',         1);
