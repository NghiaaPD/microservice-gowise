--
-- PostgreSQL database dump
--

\restrict a2PcoXH28DAZV6Agpx02JdG7LvGnZFFyupT7l4zTcmIp1GouU5Mb6PqgITJRgEs

-- Dumped from database version 17.5 (Debian 17.5-1.pgdg120+1)
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: nghiapd
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    user_id uuid,
    token text NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.refresh_tokens OWNER TO nghiapd;

--
-- Name: users; Type: TABLE; Schema: public; Owner: nghiapd
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(20) DEFAULT 'user'::character varying,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    refresh_token character varying(255),
    refresh_token_expiry timestamp(6) without time zone,
    token_expiry timestamp(6) without time zone,
    verification_token character varying(255),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['user'::character varying, 'admin'::character varying, 'collaborator'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO nghiapd;

--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: nghiapd
--

COPY public.refresh_tokens (id, user_id, token, expires_at, created_at) FROM stdin;
518bdb18-000a-4b0a-8945-f486cc8cd69e	c45c31b2-1512-4a00-be20-7efef1021c37	3f789c78-9ad3-4958-bbcc-27afefdd0c16	2025-07-21 08:35:52.176193	2025-07-14 08:35:52.1762
f33c1d00-c791-44fc-815e-d87c76447a2e	4d731553-df3f-4749-90ff-281ae1c8fc59	1a540f82-c3a4-42d5-a3e9-9fb58838faf3	2025-08-11 13:00:06.89598	2025-08-04 13:00:06.895988
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: nghiapd
--

COPY public.users (id, username, email, password_hash, role, is_active, created_at, updated_at, refresh_token, refresh_token_expiry, token_expiry, verification_token) FROM stdin;
2d5651e7-8dc5-44e1-a180-7ff086cb8c64	sadas	test_2@example.com	$2a$10$P9Mas/93Q4/QxDvizvUsSOFKLq2GvCgP1w9MITBFdkelEJ9KWqvhW	user	t	2025-06-29 06:47:23.524558	2025-06-29 06:47:23.524559	\N	\N	\N	\N
c45c31b2-1512-4a00-be20-7efef1021c37	nghiapd	123baconsau6969@gmail.com	$2a$10$XmWSkLajZi856bmtuAslJeoEgBjjblEtjlRU/S3VLEhMaXSsmueN.	user	t	2025-07-12 07:20:42.448778	2025-07-12 07:20:42.448778	\N	\N	\N	\N
4d731553-df3f-4749-90ff-281ae1c8fc59	nghiapd113	phanducnghiat1@gmail.com	$2a$10$JHlWUim8E82gOxbbZlukw.OZTre55X6Fr7rryTAphK1JMC7VlpFmi	user	t	2025-07-12 07:35:05.287549	2025-07-12 07:35:05.287549	\N	\N	\N	\N
3758a4ed-c699-48d1-9900-217c158340d2	admin	123baconsau696@gmail.com	$2a$10$iGDAWd1GsoZk3Y84nefl9uWSChb7iZuMjsCQJvK3eGOzkhIHclaGa	user	f	2025-07-12 12:56:27.644422	2025-07-12 12:56:27.644422	\N	\N	2025-07-13 12:56:27.633258	058d65d5-c345-4d49-8f9d-8a63faa43fa4
26234173-8f3f-4e48-b4bf-bad3901882a4	TuongVy	vungoctuongvy211@gmail.com	$2a$10$YXcTf.Ic/hPTAp.xpHn5f.gc2jP60P4W2pCPv9/p6TyvHMcVbzA1a	user	f	2025-07-14 08:18:01.153479	2025-07-14 08:18:01.153479	\N	\N	2025-07-15 08:18:01.107232	b3a1890d-f52a-4ecc-9ef6-f8302c66bc3f
\.


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: nghiapd
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: nghiapd
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: nghiapd
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: nghiapd
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: nghiapd
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict a2PcoXH28DAZV6Agpx02JdG7LvGnZFFyupT7l4zTcmIp1GouU5Mb6PqgITJRgEs

