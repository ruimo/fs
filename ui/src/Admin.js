import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Admin.css';

import Login from "./Login";

class Admin extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loginNeeded: false
    };
  }

  async componentDidMount() {
    try {
      const resp = await fetch("/admin");
      if (resp.status === 401) {
        console.log("Login needed");
        this.setState({
          loginNeeded: true
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    try {
      const resp = await fetch(
        "/messages", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
          body: JSON.stringify({
            keyArgs: [
              { key: 'menu' },
              { key: 'admin' }
            ]
          })
        }
      );

      if (resp.status === 200) {
        this.setState({
          messages: await resp.json()
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  msg(key) {
    return this.state.messages[key];
  }

  render() {
    const body = () => {
      if (this.state.loginNeeded) {
        return <Login url="/admin"/>;
      } else {
        return <span>{this.msg('admin')}{this.msg('menu')}</span>;
      }
    };

    return (
      <div className="admin">
        { body() }
      </div>
    );
  }
}

export default withRouter(Admin);
