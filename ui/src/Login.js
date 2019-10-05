import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './Login.css';

class Login extends Component {
  constructor(props) {
    super(props);
    console.log("url: " + props.nextUrl);
    this.state = {
      userName: "",
      password: ""
    };
  }

  componentDidMount = async() => {
    try {
      const resp = await fetch("/startLogin");

      if (resp.status === 200) {
        const json = await resp.json();
        console.log("login json: " + JSON.stringify(json));
        this.setState({
          message: json['message']
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  login = async(e) => {
    e.preventDefault();
    try {
      const resp = await fetch(
        "/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
          body: JSON.stringify({
            userName: this.state.userName,
            password: this.state.password
          })
        }
      );

      console.log("status: " + resp.status);
      if (resp.status === 200) {
        this.props.history.push("/" + this.props.match.params.nextUrl);
        if (this.props.onLoginSuccess !== undefined)
          this.props.onLoginSuccess(this.state.userName);
      } else if (resp.status === 400) {
        const json = await resp.json();
        this.setState({
          globalError: json[''],
          userNameError: json['userName'],
          passwordError: json['password']
        });
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }
  
  render() {
    const userError = this.state.userNameError !== undefined ?
      <div className="error">
        { this.state.userNameError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const passwordError = this.state.passwordError !== undefined ?
      <div className="error">
        { this.state.passwordError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    return (
      <div className="login">
        { this.state.message !== undefined &&
          <article className="message is-info">
            <div className="message-body">{this.state.message}</div>
          </article>
        }

        { this.state.globalError !== undefined && 
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>
        }

        <form onSubmit={(e) => {this.login(e)}}>
          <div className="field">
            <div className="control">
              <label className="label">User</label>
              <div className="control">
                <input className="input" type="text" placeholder="Text input" value={this.state.userName}
                       id="userName" onChange={(e) => this.setState({userName: e.target.value})}
                />
              </div>
              { userError }
            </div>
          </div>

          <div className="field">
            <div className="control">
              <label className="label">Password</label>
              <input className="input" type="password" placeholder="Password" value={this.state.password}
                     id="password" onChange={(e) => this.setState({password: e.target.value})}
              />
              { passwordError }
            </div>
          </div>

          <div className="field">
            <p className="control">
              <input type="submit" id="loginButton" className="button is-success" value="Login"/>
            </p>
          </div>
        </form>
      </div>
    );
  }
}

export default withRouter(Login);
