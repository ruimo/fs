import React, {Component} from 'react';
import {
  BrowserRouter as Router,
  Route,
  NavLink,
  Link
} from 'react-router-dom';

import Admin from "./Admin";

import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {title: ''};
  }

  render() {
    return (
      <Router>
        <div className="has-text-light">
          <nav className="navbar" role="navigation" area-label="main navigation">
            <div className="navbar-brand">
              <Link to="/" className="navbar-item">
                <i className="fas fa-music menu-icon"></i>
                <span>First Saturday</span>
              </Link>

              <div role="button" className="navbar-burger" area-label="menu" aria-expanded="false" data-target="navMenu">
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
              </div>
            </div>

            <div className="navbar-menu" id="navMenu">
              <div className="navbar-start">
              </div>
              <div className="navbar-end">
              </div>
            </div>
          </nav>
          <main className="columns">
            <div className="submenu column is-3">
              <aside className="box menu">
                <p className="menu-label">Function</p>
                <ul className="menu-list">
                  <li className="menu-item">
                    <NavLink to="/library/" activeClassName="is-active">
                      <i className="fas fa-database menu-icon"></i>
                      <span>Library</span>
                    </NavLink>
                  </li>
                </ul>
              </aside>
            </div>

            <div className="column">
              <Route path="/admin" component={Admin}/>
            </div>
          </main>
          <div className="footer">
            FOOTER
          </div>
        </div>
      </Router>
    );
  }
}

export default App;
