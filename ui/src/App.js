import React, {Component} from 'react';
import {
  BrowserRouter as Router,
  Route,
  Link
} from 'react-router-dom';

import Admin from "./Admin";

import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavShown: false
    };
  }

  toggleNav = () => {
    this.setState(prevState => ({
      isNavShown: !prevState.isNavShown
    }));
  }

  onLoginSuccess = (userName) => {
    console.log("App.onLoginSuccess: " + userName );
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

              <div role="button" className="navbar-burger" area-label="menu" aria-expanded="false" data-target="navMenu"
                   onClick={this.toggleNav}>
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
              </div>
            </div>

            <div className={this.state.isNavShown ? 'navbar-menu is-active' : 'navbar-menu'} id="navMenu">
              <div className="navbar-start">
                <div>Nav menu start</div>
              </div>
              <div className="navbar-end">
                <div>Nav menu end 0</div>
                <div>Nav menu end 1</div>
              </div>
            </div>
          </nav>
          <main className="columns">
            <div className="column">
              <Route path="/admin" render={() => <Admin onLoginSuccess={this.onLoginSuccess}/>}/>
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
