-- Stage 1: the watch-party microservice now owns the entire session domain (in Redis),
-- and the backend keeps no control-plane copy. Nothing outside the (now-deleted) backend
-- `watchparty` module ever read this table — it was a pure duplicate of live Redis state —
-- so dropping it removes the dual-write, the FK that blocked user/movie deletion, and the
-- orphaned-row problem in one step. The party_id index is dropped with the table.
DROP TABLE IF EXISTS watch_parties;
