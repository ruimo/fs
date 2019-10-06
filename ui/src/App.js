import React, {Component} from 'react';
import {
  BrowserRouter as Router,
  Route,
  Link
} from 'react-router-dom';
import MessagesLoader from "./MessagesLoader";
import Admin from "./Admin";
import Login from "./Login";
import Attend from "./Attend";
import Site from "./Site";
import SiteList from "./SiteList";
import LogoffButton from "./LogoffButton";
import AgentRecords from "./AgentRecords";
import "@fortawesome/fontawesome-free/js/all.min.js"

import './App.sass';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavShown: false,
      loginUser: undefined,
      messages: MessagesLoader.Empty,
      version: "???"
    };
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  componentDidMount = async() => {
    try {
      const resp = await fetch("/version");
      if (resp.status === 200) {
        const json = await resp.json();
        console.log("version: " + JSON.stringify(json));
        this.setState({
          version: json.version
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    try {
      const resp = await fetch("/loginInfo");
      if (resp.status === 200) {
        const json = await resp.json();
        console.log("loginInfo: " + JSON.stringify(json));
        this.setState({
          loginUser: json.user === undefined ? undefined : json.user.name
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'adminMenu'},
          { key: 'logoff'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }
  
  logoff = async() => {
    try {
      const resp = await fetch(
        "/logoff", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
        }
      );
      if (resp.status === 200) {
        this.setState({
          loginUser: undefined
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  adminMenu = () => {
    this.props.history.push("/admin");
  }

  toggleNav = () => {
    this.setState(prevState => ({
      isNavShown: !prevState.isNavShown
    }));
  }

  onLoginSuccess = (userName) => {
    console.log("App.onLoginSuccess: " + userName );
    this.setState({
      loginUser: userName
    });
  }

  onLogoffSuccess = () => {
    this.setState({
      loginUser: undefined
    });
  }

  render() {
    return (
      <Router>
        <div className="has-text-light">
          <nav className="navbar" role="navigation" area-label="main navigation">
            <div className="navbar-brand">
              <Link to="/" className="navbar-item">
                <i className="brand-icon fas fa-walking"></i>
                <span className="brand-title">First Saturday</span>
              </Link>

              <div role="button" className={this.state.isNavShown ? "navbar-burger is-active" : "navbar-burger"}
                   area-label="menu" aria-expanded="false" data-target="navMenu"
                   onClick={this.toggleNav}>
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
                <span className="" aria-hidden="true"></span>
              </div>
            </div>

            <div className={this.state.isNavShown ? 'navbar-menu is-active' : 'navbar-menu'} id="navMenu">
              <div className="navbar-start">
              </div>
              <div className="navbar-end">
                <div className="navbar-item">
                  <Link to="/admin" id="adminLink">{this.state.messages('adminMenu')}</Link>
                </div>
                {this.state.loginUser === undefined ? "" :
                 <div className="navbar-item">
                   <LogoffButton onLogoffSuccess={this.onLogoffSuccess}/>
                 </div>
                }
              </div>
            </div>
          </nav>
          <main className="columns">
            <div className="column">
              <Route exact path="/" render={() => <SiteList/>}/>
              <Route path="/admin" exact render={() => <Admin/>}/>
              <Route path="/site" exact render={() => <Site/>}/>
              <Route path="/login/:nextUrl" render={() => <Login onLoginSuccess={this.onLoginSuccess}/>}/>
              <Route path="/attend/:siteId" render={() => <Attend/>}/>
              <Route path="/agentRecords/:siteId" render={() => <AgentRecords/>}/>
            </div>
          </main>
          <div className="footer">
            <div>{this.state.version}</div>
            <div>
              First Saturday Helper
            </div>
            <div>
              S.Hanai
            </div>
          </div>
        </div>
      </Router>
    );
  }
}

export default App;
