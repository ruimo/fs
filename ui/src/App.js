import React, {Component} from 'react';
import {
  BrowserRouter as Router,
  Route,
  Link
} from 'react-router-dom';
import MessagesLoader from "./MessagesLoader";
import Admin from "./Admin";
import LogoffButton from "./LogoffButton";

import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavShown: false,
      loginUser: undefined,
      messages: MessagesLoader.Empty
    };
  }

  componentDidMount = async() => {
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
          { key: 'logoff'},
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
                <i className="fas fa-music menu-icon"></i>
                <span>First Saturday</span>
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
                <div>{
                  this.state.loginUser === undefined ?
                    <Link to="/admin">{this.state.messages('adminMenu')}</Link> :
                    <LogoffButton onLogoffSuccess={this.onLogoffSuccess}/>
                }</div>
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
