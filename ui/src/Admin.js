import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Admin.css';
import MessagesLoader from "./MessagesLoader";

import Login from "./Login";

class Admin extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loginNeeded: false,
      messages: MessagesLoader.Empty
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
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'adminMenu'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  onLoginSuccess = (userName) => {
    console.log("Admin.onLoginSuccess: " + userName );
    if (this.props.onLoginSuccess !== undefined)
      this.props.onLoginSuccess(userName);
    this.setState({
      loginNeeded: false
    });
    this.props.history.push("/admin");
  }

  render() {
    const body = () => {
      if (this.state.loginNeeded) {
        return <Login url="/admin" onLoginSuccess={this.onLoginSuccess}/>;
      } else {
        return <span>{this.state.messages('adminMenu')}</span>;
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
