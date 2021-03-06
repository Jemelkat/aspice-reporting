create sequence source_data_source_data_id_seq
    increment by 25;

alter sequence source_data_source_data_id_seq owner to postgres;

create table roles
(
    role_id bigint generated by default as identity
        primary key,
    name    varchar(20)
);

alter table roles
    owner to postgres;

create table user_group
(
    group_id   bigint generated by default as identity
        primary key,
    group_name varchar(255)
        constraint uk_7mwr7k8eyya6no8uj3eiq2d3c
            unique
);

alter table user_group
    owner to postgres;

create table users
(
    user_id  bigint generated by default as identity
        primary key,
    email    varchar(60)
        constraint uk6dotkott2kjsp8vw4d0m25fb7
            unique,
    password varchar(120),
    username varchar(20)
        constraint ukr43af9ap4edm43mmtq01oddj6
            unique
);

alter table users
    owner to postgres;

create table dashboard
(
    dashboard_id bigint generated by default as identity
        primary key,
    user_id      bigint not null
        constraint fklusqdmhtffg9gl024rqtguufb
            references users
);

alter table dashboard
    owner to postgres;

create table report
(
    report_id           bigint generated by default as identity
        primary key,
    report_created      timestamp,
    report_last_updated timestamp,
    report_name         varchar(50),
    user_id             bigint not null
        constraint fkq50wsn94sc3mi90gtidk0k34a
            references users,
    constraint ukrmjbsm61jfvos6migkg4dttfd
        unique (report_name, user_id)
);

alter table report
    owner to postgres;

create table source
(
    source_id      bigint generated by default as identity
        primary key,
    source_created timestamp,
    source_name    varchar(255),
    user_id        bigint not null
        constraint fki56jbsjqlnp0j0xjmi8d2qmeg
            references users,
    constraint ukbp4it9haoxtmksuwqy3k15sec
        unique (source_name, user_id)
);

alter table source
    owner to postgres;

create table score_range
(
    range_id  bigint generated by default as identity
        primary key,
    l         double precision,
    lminus    double precision,
    lplus     double precision,
    mode      varchar(50),
    n         double precision,
    p         double precision,
    pminus    double precision,
    pplus     double precision,
    source_id bigint
        constraint uk_a258rovbk5f0u9p11ukfd76i6
            unique
        constraint fkt6a6ftr1lw2usrhrexyn0remc
            references source
);

alter table score_range
    owner to postgres;

create table source_groups
(
    source_id bigint not null
        constraint fk2gd3pjpv798nmbkh3gw0gxg41
            references source,
    group_id  bigint not null
        constraint fkkbww9a3ra7pyoxlqqfvw8q8l9
            references user_group,
    primary key (source_id, group_id)
);

alter table source_groups
    owner to postgres;

create table source_column
(
    source_column_id bigint generated by default as identity
        primary key,
    column_name      varchar(255),
    source_id        bigint not null
        constraint fkppky8lhnfyilppk1lijb7mea2
            references source,
    column_ordinal   integer
);

alter table source_column
    owner to postgres;

create table source_data
(
    source_data_id    bigint not null
        primary key,
    source_data_value varchar(255),
    source_column_id  bigint not null
        constraint fkq82bxkacudf3dblua3pg13mwt
            references source_column,
    column_ordinal    integer
);

alter table source_data
    owner to postgres;

create table template
(
    template_id           bigint generated by default as identity
        primary key,
    orientation           varchar(20) not null,
    template_created      timestamp,
    template_last_updated timestamp,
    template_name         varchar(50),
    user_id               bigint
        constraint fkdjl177n13c7qmsoxp434nkml0
            references users,
    constraint ukn2r7cjkxmxmjgn3fpagydinvp
        unique (template_name, user_id)
);

alter table template
    owner to postgres;

create table report_page
(
    report_page_id   bigint generated by default as identity
        primary key,
    orientation      varchar(20) not null,
    page_template_id bigint
        constraint fkk7cdh315q0qeo2ff1fx6vsidb
            references template,
    report_id        bigint      not null
        constraint fklr9s0df9us45e1w8jsing5pck
            references report,
    pages_ordinal    integer
);

alter table report_page
    owner to postgres;

create table report_item
(
    report_item_type   varchar(31) not null,
    report_item_id     bigint generated by default as identity
        primary key,
    report_item_height integer,
    report_item_width  integer,
    report_item_x      integer,
    report_item_y      integer,
    dashboard_id       bigint
        constraint fkno8mlid32qslls1lhq93l2i86
            references dashboard,
    report_page_id     bigint
        constraint fkthifh6fxviawrbjycdr3txko6
            references report_page,
    template_id        bigint
        constraint fkksv7vncaxc2onr0s6sj019oqu
            references template
);

alter table report_item
    owner to postgres;

create table capability_table
(
    aggregate_scores    varchar(20) not null,
    assessor_filter     text[],
    criterion_width     integer     not null,
    font_size           integer,
    level_limit         integer     not null,
    process_filter      text[],
    process_width       integer     not null,
    specific_level      integer,
    report_item_id      bigint      not null
        primary key
        constraint fkpwe1p0npng804hac4syycqq13
            references report_item,
    assessor_column_id  bigint
        constraint fkrw03y4jhn60fp9fcxrlfjuf7b
            references source_column,
    criterion_column_id bigint
        constraint fkl4yr2xfbdikihqfo7qd56v28s
            references source_column,
    level_column_id     bigint
        constraint fkakk921mlvoit50axe3oik498n
            references source_column,
    process_column_id   bigint
        constraint fkkijd4kut31kkme641aw3mwety
            references source_column,
    score_column_id     bigint
        constraint fk770yjacy7xqflxfuw4ntbehip
            references source_column,
    source_source_id    bigint
        constraint fk1a77e1t73lss2dvpod86qct8j
            references source
);

alter table capability_table
    owner to postgres;

create table level_bar_graph
(
    aggregate_levels      boolean,
    aggregate_scores      varchar(20) not null,
    aggregate_sources     varchar(20) not null,
    assessor_column_name  varchar(255),
    assessor_filter       text[],
    attribute_column_name varchar(255),
    criterion_column_name varchar(255),
    orientation           varchar(20) not null,
    process_column_name   varchar(255),
    process_filter        text[],
    score_column_name     varchar(255),
    title                 varchar(255),
    report_item_id        bigint      not null
        primary key
        constraint fk2aaxxea3t4mwpugak3wxfpc2m
            references report_item
);

alter table level_bar_graph
    owner to postgres;

create table bar_graph_sources
(
    report_item_id     bigint  not null
        constraint fkd77ndl9h47qr610a98rnhej2h
            references level_bar_graph,
    source_id          bigint  not null
        constraint fkbsp29edpdktje7rh1xcr7x1vu
            references source,
    graph_source_order integer not null,
    primary key (report_item_id, graph_source_order)
);

alter table bar_graph_sources
    owner to postgres;

create table level_pie_graph
(
    aggregate_levels    boolean,
    aggregate_scores    varchar(20) not null,
    assessor_filter     text[],
    title               varchar(255),
    report_item_id      bigint      not null
        primary key
        constraint fk1hmm0t6o39t6yhp8id08ujr6k
            references report_item,
    assessor_column_id  bigint
        constraint fk1mtmxyxwqfit839dm5y5hg25v
            references source_column,
    attribute_column_id bigint
        constraint fk9uugc1tcwoxu21s71s3m4ik1h
            references source_column,
    criterion_column_id bigint
        constraint fkt00s2eriycnubsqqxpd5hbljt
            references source_column,
    process_column_id   bigint
        constraint fktj3uau8bl7bv7djf5h3e1xjc3
            references source_column,
    score_column_id     bigint
        constraint fk3m1ft4pyysx5cm4iootpk9l64
            references source_column,
    source_source_id    bigint
        constraint fk8u7t7t9fmkixh189ixjr4etwa
            references source
);

alter table level_pie_graph
    owner to postgres;

create table simple_table
(
    report_item_id   bigint not null
        primary key
        constraint fk58gc0j3epfjhdxa75y0p4fh3c
            references report_item,
    source_source_id bigint
        constraint fkm50kfpr4tjr9mx5vcwi9n5102
            references source
);

alter table simple_table
    owner to postgres;

create table table_column
(
    table_column_id  bigint generated by default as identity
        primary key,
    width            integer not null,
    source_column_id bigint
        constraint fkp3y50qb942ov2e0pp5542by96
            references source_column,
    simple_table_id  bigint
        constraint fkbgybjgd6hayljwt3rtqnwevlk
            references simple_table,
    column_ordinal   integer
);

alter table table_column
    owner to postgres;

create table text_item
(
    text_area      text,
    report_item_id bigint not null
        primary key
        constraint fkcabtnu4yk0dsed14h46odnycq
            references report_item
);

alter table text_item
    owner to postgres;

create table text_style
(
    style_id                 bigint generated by default as identity
        primary key,
    bold                     boolean,
    color                    varchar(255),
    font_size                integer,
    italic                   boolean,
    underline                boolean,
    text_item_report_item_id bigint
        constraint fkb6nntmvr3t4lfehbyw3ae1ae
            references text_item
);

alter table text_style
    owner to postgres;

create table user_groups
(
    user_id  bigint not null
        constraint fkd37bs5u9hvbwljup24b2hin2b
            references users,
    group_id bigint not null
        constraint fk8e19i1id24cdt0qromtwlmlrg
            references user_group,
    primary key (user_id, group_id)
);

alter table user_groups
    owner to postgres;

create table user_roles
(
    user_id bigint not null
        constraint fkhfh9dx7w3ubf1co1vdev94g3f
            references users,
    role_id bigint not null
        constraint fkh8ciramu9cc9q3qcqiv4ue8a6
            references roles,
    primary key (user_id, role_id)
);

alter table user_roles
    owner to postgres;

