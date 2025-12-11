import React, { useState, useEffect } from 'react';
// import followedForumsMock from '../data/followed-forums.jsx';
import { getFollowedForumsApi } from '../api/forums-api.jsx';
import './style/FollowedForums.css';
import { Link } from 'react-router-dom';
import {PATHS} from "../constants/constants.jsx";

const DEFAULT_VISIBLE = 5;

const FollowedForums = ({ maxVisible = DEFAULT_VISIBLE }) => {
  const [expanded, setExpanded] = useState(false);

  const [forums, setForums] = useState([]);
  const visible = expanded ? forums.length : Math.min(maxVisible, forums.length);

  const handleToggle = () => {
    setExpanded(prev => !prev);
  };

  useEffect(() => {
    let mounted = true;
    async function fetchData(){
      const res = await getFollowedForumsApi({ page: 0, size: 10 });
      if (mounted && res?.success) {
        setForums(res.data.forums);
      }
    }
    fetchData();
    return () => { mounted = false; }
  }, []);

  return (
    <div className="followed-forums">
      <div className="followed-forums-header">Followed forums</div>
      <div className="followed-forums-list">
        {forums.slice(0, visible).map(f => (
          <Link to={PATHS.FORUM.PAGE(f.id)} key={f.id} className="followed-forum-item" title={f.name}>
            <div className="forum-avatar">
              {f.avatar ? <img src={f.avatar} alt={f.name} /> : <div className="forum-initials">{f.name.split(' ').map(n => n[0]).slice(0,2).join('')}</div>}
            </div>
            <div className="forum-name">{f.name}</div>
          </Link>
        ))}
      </div>
      {forums.length > maxVisible && (
        <button className="show-more" onClick={handleToggle}>{expanded ? 'Show less ▲' : 'Show more ▼'}</button>
      )}
    </div>
  );
};

export default FollowedForums;